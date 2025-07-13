#ifndef ERSAP_DEMO_SRO_PRINTER_SERVICE_HPP_
#define ERSAP_DEMO_SRO_PRINTER_SERVICE_HPP_

#include <ersap/engine.hpp>
#include <memory>

namespace ersap {
namespace demo {

class SROPrinterService : public ersap::Engine {
public:
    ersap::EngineData configure(ersap::EngineData& input) override;
    ersap::EngineData execute(ersap::EngineData& input) override;
    ersap::EngineData execute_group(const std::vector<ersap::EngineData>& group) override;

    std::vector<ersap::EngineDataType> input_data_types() const override;
    std::vector<ersap::EngineDataType> output_data_types() const override;
    std::set<std::string> states() const override;

    std::string name() const override;
    std::string author() const override;
    std::string description() const override;
    std::string version() const override;
};

} // namespace demo
} // namespace ersap

#endif // ERSAP_DEMO_SRO_PRINTER_SERVICE_HPP_
