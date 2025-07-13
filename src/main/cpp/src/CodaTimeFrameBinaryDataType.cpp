// CodaTimeFrameBinaryDataType.cpp
#include "CodaTimeFrameBinaryDataType.hpp"
#include <ersap/any.hpp>

namespace ersap {
namespace coda {

// MIME type constant matching Java implementation
const std::string CODA_TIME_FRAME_BINARY_MIME_TYPE = "binary/coda-time-frame";

// The ERSAP EngineDataType for binary CodaTimeFrame
const ersap::EngineDataType CODA_TIME_FRAME_BINARY_TYPE{
    CODA_TIME_FRAME_BINARY_MIME_TYPE,
    std::make_unique<CodaTimeFrameBinarySerializer>()
};

// Binary serializer implementation
std::vector<std::uint8_t> CodaTimeFrameBinarySerializer::write(const ersap::any& data) const {
    const auto& event = ersap::any_cast<const CodaTimeFrame&>(data);
    return CodaTimeFrameSerializer::serializeToBinary(event);
}

ersap::any CodaTimeFrameBinarySerializer::read(const std::vector<std::uint8_t>& buffer) const {
    CodaTimeFrame event = CodaTimeFrameSerializer::deserializeFromBinary(buffer);
    return ersap::any{std::move(event)};
}

// Utility functions that delegate to CodaTimeFrameSerializer
std::vector<std::uint8_t> serializeToBinary(const CodaTimeFrame& event) {
    return CodaTimeFrameSerializer::serializeToBinary(event);
}

CodaTimeFrame deserializeFromBinary(const std::vector<std::uint8_t>& buffer) {
    return CodaTimeFrameSerializer::deserializeFromBinary(buffer);
}

} // namespace coda
} // namespace ersap
