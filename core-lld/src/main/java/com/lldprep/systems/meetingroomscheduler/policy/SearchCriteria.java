package com.lldprep.systems.meetingroomscheduler.policy;

import com.lldprep.systems.meetingroomscheduler.model.Amenity;
import com.lldprep.systems.meetingroomscheduler.model.TimeSlot;

import java.util.Optional;
import java.util.Set;

/** A search query. {@code floor} empty = any floor; empty amenities = no amenity constraint. */
public record SearchCriteria(TimeSlot timeSlot, int minCapacity,
                             Optional<Integer> floor, Set<Amenity> requiredAmenities) {

    public SearchCriteria {
        floor = floor == null ? Optional.empty() : floor;
        requiredAmenities = requiredAmenities == null ? Set.of() : Set.copyOf(requiredAmenities);
    }
}
