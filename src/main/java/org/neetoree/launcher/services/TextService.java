package org.neetoree.launcher.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.util.ResourceBundle;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
@Singleton
public class TextService {
    @Inject
    @Named("text")
    private ResourceBundle textBundle;

    public String text(String key) {
        return textBundle.getString(key);
    }
}
