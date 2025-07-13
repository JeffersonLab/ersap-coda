#ifndef ERSAP_DEMO_SRO_HPP_
#define ERSAP_DEMO_SRO_HPP_

#include <ersap/engine_data_type.hpp>
#include <vector>
#include <cstdint>
#include <string>
#include <sstream>

class FADCHit {
public:
    FADCHit(int crate, int slot, int channel, int charge, int64_t time)
        : crate_{crate}, slot_{slot}, channel_{channel}, charge_{charge}, time_{time} {}

    int crate() const { return crate_; }
    int slot() const { return slot_; }
    int channel() const { return channel_; }
    int charge() const { return charge_; }
    int64_t time() const { return time_; }

    std::string toString() const {
        std::ostringstream oss;
        oss << "FADCHit{"
            << "crate=" << crate_ << ", "
            << "slot=" << slot_ << ", "
            << "channel=" << channel_ << ", "
            << "charge=" << charge_ << ", "
            << "time=" << time_ << "}";
        return oss.str();
    }

private:
    int crate_;
    int slot_;
    int channel_;
    int charge_;
    int64_t time_;
};

class RocTimeFrameBank {
public:
    int getRocID() const { return roc_id_; }
    void setRocID(int id) { roc_id_ = id; }

    int getFrameNumber() const { return frame_number_; }
    void setFrameNumber(int num) { frame_number_ = num; }

    int64_t getTimeStamp() const { return time_stamp_; }
    void setTimeStamp(int64_t ts) { time_stamp_ = ts; }

    const std::vector<FADCHit>& getHits() const { return hits_; }
    void addHit(const FADCHit& hit) { hits_.push_back(hit); }

    std::string toString() const {
        std::ostringstream oss;
        oss << "RocTimeFrameBank{"
            << "rocID=" << roc_id_ << ", "
            << "frameNumber=" << frame_number_ << ", "
            << "timeStamp=" << time_stamp_ << ", "
            << "hits=[";
        for (const auto& hit : hits_) {
            oss << hit.toString() << ", ";
        }
        oss << "]}";
        return oss.str();
    }

private:
    int roc_id_;
    int frame_number_;
    int64_t time_stamp_;
    std::vector<FADCHit> hits_;
};
extern const ersap::EngineDataType SRO_TYPE;
#endif // ERSAP_DEMO_SRO_HPP_
