package com.example.healthmet;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test strumentale, che verrà eseguito su un dispositivo Android.
 *
 * @see <a href="http://d.android.com/tools/testing">Documentazione sul testing</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context dell'applicazione in test.
        Context appContext = ApplicationProvider.getApplicationContext();
        assertEquals("com.example.healthmet", appContext.getPackageName());
    }
}
