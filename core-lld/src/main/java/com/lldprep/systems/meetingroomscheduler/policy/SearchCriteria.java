package com.lldprep.systems.meetingroomscheduler.policy;

import com.lldprep.systems.meetingroomscheduler.model.Amenity;
import com.lldprep.systems.meetingroomscheduler.model.TimeSlot;

import java.util.Set;

public record SearchCriteria(TimeSlot timeSlot, int minCapacity,
                             int floor, Set<Amenity> requiredAmenities, boolean floorSpecified) {

    public SearchCriteria {
        if (requiredAmenities != null) {
            requiredAmenities = Set.copyOf(requiredAmenities);
        }
    }

    public static SearchCriteria forAvailability(TimeSlot timeSlot, int minCapacity,
                                                  int floor, boolean floorSpecified,
                                                  Set<Amenity> requiredAmenities) {
        return new SearchCriteria(timeSlot, minCapacity, floor, requiredAmenities, floorSpecified);
    }
}
