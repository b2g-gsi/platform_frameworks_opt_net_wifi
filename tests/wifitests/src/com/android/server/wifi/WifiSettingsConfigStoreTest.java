/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wifi;

import static com.android.server.wifi.WifiSettingsConfigStore.WIFI_VERBOSE_LOGGING_ENABLED;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Handler;
import android.os.test.TestLooper;
import android.util.Xml;

import androidx.test.filters.SmallTest;

import com.android.internal.util.FastXmlSerializer;
import com.android.server.wifi.util.XmlUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


/**
 * Unit tests for {@link com.android.server.wifi.WifiSettingsConfigStore}.
 */
@SmallTest
public class WifiSettingsConfigStoreTest extends WifiBaseTest {
    @Mock
    private Context mContext;
    @Mock
    private WifiConfigStore mWifiConfigStore;
    @Mock
    private WifiConfigManager mWifiConfigManager;

    private TestLooper mLooper;
    private WifiSettingsConfigStore mWifiSettingsConfigStore;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mLooper = new TestLooper();
        mWifiSettingsConfigStore =
                new WifiSettingsConfigStore(mContext, new Handler(mLooper.getLooper()),
                        mWifiConfigManager, mWifiConfigStore);
    }

    /**
     * Called after each test
     */
    @After
    public void cleanup() {
        validateMockitoUsage();
    }

    @Test
    public void testSetterGetter() {
        assertFalse(mWifiSettingsConfigStore.getBoolean(WIFI_VERBOSE_LOGGING_ENABLED, false));
        mWifiSettingsConfigStore.putBoolean(WIFI_VERBOSE_LOGGING_ENABLED, true);
        mLooper.dispatchAll();
        assertTrue(mWifiSettingsConfigStore.getBoolean(WIFI_VERBOSE_LOGGING_ENABLED, false));
        verify(mWifiConfigManager).saveToStore(true);
    }

    @Test
    public void testChangeListener() {
        WifiSettingsConfigStore.OnSettingsChangedListener listener = mock(
                WifiSettingsConfigStore.OnSettingsChangedListener.class);
        mWifiSettingsConfigStore.registerChangeListener(WIFI_VERBOSE_LOGGING_ENABLED, listener,
                new Handler(mLooper.getLooper()));

        mWifiSettingsConfigStore.putBoolean(WIFI_VERBOSE_LOGGING_ENABLED, true);
        mLooper.dispatchAll();

        verify(listener).onSettingsChanged(WIFI_VERBOSE_LOGGING_ENABLED, true);
    }

    @Test
    public void testSaveAndLoadFromStore() throws Exception {
        ArgumentCaptor<WifiConfigStore.StoreData> storeDataCaptor = ArgumentCaptor.forClass(
                WifiConfigStore.StoreData.class);
        verify(mWifiConfigStore).registerStoreData(storeDataCaptor.capture());
        assertNotNull(storeDataCaptor.getValue());

        XmlPullParser in = createSettingsTestXmlForParsing(WIFI_VERBOSE_LOGGING_ENABLED, true);

        storeDataCaptor.getValue().resetData();
        storeDataCaptor.getValue().deserializeData(in, in.getDepth(), -1, null);

        assertTrue(mWifiSettingsConfigStore.getBoolean(WIFI_VERBOSE_LOGGING_ENABLED, false));
    }

    private XmlPullParser createSettingsTestXmlForParsing(String key, Object value)
            throws Exception {
        Map<String, Object> settings = new HashMap<>();
        // Serialize
        settings.put(key, value);
        final XmlSerializer out = new FastXmlSerializer();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        out.setOutput(outputStream, StandardCharsets.UTF_8.name());
        XmlUtil.writeDocumentStart(out, "Test");
        XmlUtil.writeNextValue(out, "Values", settings);
        XmlUtil.writeDocumentEnd(out, "Test");

        // Start Deserializing
        final XmlPullParser in = Xml.newPullParser();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        in.setInput(inputStream, StandardCharsets.UTF_8.name());
        XmlUtil.gotoDocumentStart(in, "Test");
        return in;
    }
}