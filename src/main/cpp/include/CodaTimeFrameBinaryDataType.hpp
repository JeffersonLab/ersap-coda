#ifndef CODA_TIME_FRAME_BINARY_DATA_TYPE_HPP
#define CODA_TIME_FRAME_BINARY_DATA_TYPE_HPP

#include "CodaTimeFrameDataType.hpp"
#include <ersap/engine_data_type.hpp>
#include <ersap/serializer.hpp>
#include <string>
#include <memory>

namespace ersap {
namespace coda {

/**
 * Binary serializer for CodaTimeFrame using the same format as Java
 * This provides fast binary serialization compatible with Java implementation
 */
class CodaTimeFrameBinarySerializer : public ersap::Serializer {
public:
    std::vector<std::uint8_t> write(const ersap::any& data) const override;
    ersap::any read(const std::vector<std::uint8_t>& buffer) const override;
};

// Binary serialization utility functions
std::vector<std::uint8_t> serializeToBinary(const CodaTimeFrame& event);
CodaTimeFrame deserializeFromBinary(const std::vector<std::uint8_t>& buffer);

// MIME type constant matching Java implementation
extern const std::string CODA_TIME_FRAME_BINARY_MIME_TYPE;

// The ERSAP EngineDataType for binary CodaTimeFrame
extern const ersap::EngineDataType CODA_TIME_FRAME_BINARY_TYPE;

} // namespace coda
} // namespace ersap

#endif // CODA_TIME_FRAME_BINARY_DATA_TYPE_HPP
