package org.neetoree.launcher.os;

import com.google.inject.Provider;
import com.google.inject.ProvisionException;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public class OsSpecificProvider implements Provider<OsSpecific> {
    private static OsSpecific[] specs = {
            new LinuxSpecific(),
            new WindowsSpecific(),
            new MacSpecific()
    };

    private static OsSpecific current;
    {
        for (OsSpecific spec : specs) {
            if (spec.current()) {
                current = spec;
                break;
            }
        }
        if (current == null) {
            throw new ProvisionException("Unknown OS");
        }
    }

    @Override
    public OsSpecific get() {
        return current;
    }
}
