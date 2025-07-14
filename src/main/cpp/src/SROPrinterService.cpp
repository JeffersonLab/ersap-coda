#include "SROTestDataType.hpp"

#include <SROPrinterService.hpp>
#include <ersap/stdlib/json_utils.hpp>
#include <ersap/engine_data_type.hpp>
#include <ersap/any.hpp>
#include <vector>
#include <cstdint>
#include <memory>
#include <stdexcept>
#include <cstring>

#include <iostream>
#include <memory>

// Define SRO_TYPE directly in this library to avoid linking issues
namespace ersap {
namespace demo {

using byte_t = std::uint8_t;
using bytes_t = std::vector<byte_t>;

class ByteBuffer {
public:
    ByteBuffer(size_t size) { data_.reserve(size); }

    void put(std::int32_t v) {
        // Write in big-endian format to match Java DataInputStream
        data_.push_back((v >> 24) & 0xFF);
        data_.push_back((v >> 16) & 0xFF);
        data_.push_back((v >> 8) & 0xFF);
        data_.push_back(v & 0xFF);
    }

    void put(std::int64_t v) {
        // Write in big-endian format to match Java DataInputStream
        for (int i = 7; i >= 0; --i) {
            data_.push_back((v >> (i * 8)) & 0xFF);
        }
    }

    template<typename T>
    void putRange(const T& v) {
        data_.insert(data_.end(), v.begin(), v.end());
    }

    const bytes_t& data() const { return data_; }

public:
    bytes_t data_;
};

class SROSerializer : public ersap::Serializer {
public:
    std::vector<std::uint8_t> write(const ersap::any& data) const override {
        const auto& sro = ersap::any_cast<const std::vector<std::vector<RocTimeFrameBank>>&>(data);

        ByteBuffer buffer(8192); // starting with an arbitrary size
        buffer.put((int32_t)sro.size());

        for (const auto& sublist : sro) {
            buffer.put((int32_t)sublist.size());
            for (const auto& frame : sublist) {
                buffer.put((int32_t)frame.getRocID());
                buffer.put((int32_t)frame.getFrameNumber());
                buffer.put((int64_t)frame.getTimeStamp());

                const auto& hits = frame.getHits();
                buffer.put((int32_t)hits.size());
                for (const auto& hit : hits) {
                    buffer.put((int32_t)hit.crate());
                    buffer.put((int32_t)hit.slot());
                    buffer.put((int32_t)hit.channel());
                    buffer.put((int32_t)hit.charge());
                    buffer.put((int64_t)hit.time());
                }
            }
        }

        return buffer.data_;
    }

    ersap::any read(const std::vector<std::uint8_t>& buffer) const override {
        size_t i = 0;
        
        if (buffer.size() < 4) {
            throw std::runtime_error("Buffer too small for SRO data");
        }
        
        int32_t outer_size = to_int(buffer, i); 
        i += 4;
        
        if (outer_size < 0 || outer_size > 1000000) {
            throw std::runtime_error("Invalid outer_size: " + std::to_string(outer_size));
        }
        
        std::vector<std::vector<RocTimeFrameBank>> sro;
        sro.reserve(outer_size);

        for (int32_t o = 0; o < outer_size; ++o) {
            int32_t inner_size = to_int(buffer, i); i += 4;
            std::vector<RocTimeFrameBank> sublist;
            sublist.reserve(inner_size);

            for (int32_t f = 0; f < inner_size; ++f) {
                RocTimeFrameBank frame;
                frame.setRocID(to_int(buffer, i)); i += 4;
                frame.setFrameNumber(to_int(buffer, i)); i += 4;
                frame.setTimeStamp(to_long(buffer, i)); i += 8;

                int32_t hit_count = to_int(buffer, i); i += 4;
                for (int32_t h = 0; h < hit_count; ++h) {
                    int crate = to_int(buffer, i); i += 4;
                    int slot = to_int(buffer, i); i += 4;
                    int channel = to_int(buffer, i); i += 4;
                    int charge = to_int(buffer, i); i += 4;
                    long time = to_long(buffer, i); i += 8;
                    frame.addHit(FADCHit(crate, slot, channel, charge, time));
                }
                sublist.push_back(frame);
            }
            sro.push_back(sublist);
        }

        return ersap::any{sro};
    }

private:
    int32_t to_int(const bytes_t& b, size_t offset) const {
        if (offset + 3 >= b.size()) {
            throw std::runtime_error("Buffer overflow in to_int");
        }
        // Read in big-endian format to match Java DataOutputStream
        return (b[offset] << 24) | (b[offset+1] << 16) | (b[offset+2] << 8) | (b[offset+3]);
    }

    int64_t to_long(const bytes_t& b, size_t offset) const {
        if (offset + 7 >= b.size()) {
            throw std::runtime_error("Buffer overflow in to_long");
        }
        // Read in big-endian format to match Java DataOutputStream
        int64_t val = 0;
        for (int j = 0; j < 8; ++j) {
            val = (val << 8) | b[offset + j];
        }
        return val;
    }
};

const ersap::EngineDataType SRO_TYPE{"binary/sro-data", std::make_unique<SROSerializer>()};

} // namespace demo
} // namespace ersap

extern "C"
std::unique_ptr<ersap::Engine> create_engine()
{
    return std::make_unique<ersap::demo::SROPrinterService>();
}

namespace ersap {
namespace demo {

ersap::EngineData SROPrinterService::configure(ersap::EngineData& input)
{
    // Stateless service: nothing to configure
    return {};
}

ersap::EngineData SROPrinterService::execute(ersap::EngineData& input)
{
    auto output = ersap::EngineData{};

    if (input.mime_type() != SRO_TYPE) {
        output.set_status(ersap::EngineStatus::ERROR);
        output.set_description("Wrong input type");
        return output;
    }

    const auto& data = ersap::data_cast<std::vector<std::vector<RocTimeFrameBank>>>(input);

    std::cout << "Received SRO Data:\n";
    for (const auto& frameList : data) {
        for (const auto& frame : frameList) {
            // Only print frames that have hits
            if (!frame.getHits().empty()) {
                std::cout << frame.toString() << "\n";
            }
        }
    }

    output.set_data(SRO_TYPE, data);
    return output;
}

ersap::EngineData SROPrinterService::execute_group(const std::vector<ersap::EngineData>&)
{
    return {};
}

std::vector<ersap::EngineDataType> SROPrinterService::input_data_types() const
{
    return { SRO_TYPE, ersap::type::JSON };
}

std::vector<ersap::EngineDataType> SROPrinterService::output_data_types() const
{
    return { SRO_TYPE };
}

std::set<std::string> SROPrinterService::states() const
{
    return {};
}

std::string SROPrinterService::name() const
{
    return "SROPrinterService";
}

std::string SROPrinterService::author() const
{
    return "gurjyan";
}

std::string SROPrinterService::description() const
{
    return "Prints content of SRO data to console";
}

std::string SROPrinterService::version() const
{
    return "1.0";
}

} // namespace demo
} // namespace ersap
