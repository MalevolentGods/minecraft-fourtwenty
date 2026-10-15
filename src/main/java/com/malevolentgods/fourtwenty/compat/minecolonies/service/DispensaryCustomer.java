package com.malevolentgods.fourtwenty.compat.minecolonies.service;

/** Entity-owned transient task; never serialized as a visit or stored in a global world cache. */
public interface DispensaryCustomer {
    CitizenSmokingTask fourtwenty$smokingTask();
    void fourtwenty$smokingTask(CitizenSmokingTask task);
}
