#include "SROPrinterService.hpp"

#include <SROPrinterService.hpp>
#include <ersap/stdlib/json_utils.hpp>

#include <iostream>
#include <memory>

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
            std::cout << frame.toString() << "\n";
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
    return { SRO_TYPE };
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
