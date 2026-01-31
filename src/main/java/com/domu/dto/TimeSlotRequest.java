package com.domu.dto;

import java.util.List;

public class TimeSlotRequest {
    private List<TimeSlotItem> slots;

    public TimeSlotRequest() {
    }

    public List<TimeSlotItem> getSlots() {
        return slots;
    }

    public void setSlots(List<TimeSlotItem> slots) {
        this.slots = slots;
    }

    public static class TimeSlotItem {
        private Integer dayOfWeek;
        private String startTime;
        private String endTime;
        private Boolean active;

        public TimeSlotItem() {
        }

        public Integer getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(Integer dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }
}
