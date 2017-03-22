bus_guideway(e0).
escape(e0).
raceway(e0).

average_speed(0.9).

traffic_weight(R, T, 1) :- not(traffic(R, T, _)).
traffic_weight(R, T, 0.9) :- traffic(R, T, medium).
traffic_weight(R, T, 0.7) :- traffic(R, T, high).

lighting_weight(good, 1).
lighting_weight(medium, 0.9).
lighting_weight(low, 0.8).

lighting(_, morning, good).
lighting(_, noon, good).
lighting(R, afternoon, medium) :- lit(R).
lighting(R, afternoon, low) :- not(lit(R)).

forCars(R) :- not(service(R)), not(pedestrian(R)), not(track(R)), not(bus_guideway(R)), not(escape(R)), not(raceway(R)), not(footway(R)), not(bridleway(R)), not(steps(R)), not(path(R)), not(cycleway(R)), not(waterway(R)), not(railway(R)).
accessible(R) :- access(R), forCars(R).

priority(R, T, Z) :- accessible(R), maxspeed(R, MS), average_speed(A), traffic_weight(R, T, TW), lighting(R, T, L), lighting_weight(L, LW), Z is MS * A * TW * LW.
priority(R, _, 0) :- not(accessible(R)).



