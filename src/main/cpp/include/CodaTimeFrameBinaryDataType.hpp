#ifndef CODA_TIME_FRAME_BINARY_DATA_TYPE_HPP
#define CODA_TIME_FRAME_BINARY_DATA_TYPE_HPP

#include "CodaTimeFrameDataType.hpp"
#include <string>

namespace ersap {
namespace coda {

// Binary serialization utility functions
std::vector<std::uint8_t> serializeToBinary(const CodaTimeFrame& event);
CodaTimeFrame deserializeFromBinary(const std::vector<std::uint8_t>& buffer);

const std::string CODA_TIME_FRAME_BINARY_MIME_TYPE = "binary/coda-time-frame";

} // namespace coda
} // namespace ersap

#endif // CODA_TIME_FRAME_BINARY_DATA_TYPE_HPP
