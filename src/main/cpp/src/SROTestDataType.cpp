#ifndef ERSAP_DEMO_DATA_SRO_HPP_
#define ERSAP_DEMO_DATA_SRO_HPP_

#include <ersap/engine_data_type.hpp>
#include <ersap/any.hpp>
#include <vector>
#include <cstdint>
#include <memory>
#include <stdexcept>
#include <cstring>
#include <SROTestDataType.hpp>  // Assume this includes RocTimeFrameBank and FADCHit definitions

namespace ersap {
namespace demo {

using byte_t = std::uint8_t;
using bytes_t = std::vector<byte_t>;

class ByteBuffer {
public:
    ByteBuffer(size_t size) { data_.reserve(size); }

    void put(std::int32_t v) {
        data_.insert(data_.end(), (byte_t*) &v, (byte_t*) &v + sizeof(v));
    }

    void put(std::int64_t v) {
        data_.insert(data_.end(), (byte_t*) &v, (byte_t*) &v + sizeof(v));
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
        int32_t outer_size = to_int(buffer, i); i += 4;
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
        return (b[offset+3] << 24) | (b[offset+2] << 16) | (b[offset+1] << 8) | (b[offset]);
    }

    int64_t to_long(const bytes_t& b, size_t offset) const {
        int64_t val = 0;
        for (int j = 7; j >= 0; --j) {
            val = (val << 8) | b[offset + j];
        }
        return val;
    }
};

const ersap::EngineDataType SRO_TYPE{"binary/sro-data", std::make_unique<SROSerializer>()};

} // namespace demo
} // namespace ersap

#endif
