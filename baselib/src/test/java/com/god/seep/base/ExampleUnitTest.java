package com.god.seep.base;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

@RunWith(MockitoJUnitRunner.class)
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        System.out.println("test a");
        assertTrue(1 == 1);
        assertEquals(4, 2 + 2);
    }

    @Mock
    Context mContext;

    @Test
    public void readStringFromContext() {
        when(mContext.getString(R.string.app_name)).thenReturn("baseLib");
        assertThat(mContext.getString(R.string.app_name), is("baseLib"));

        when(mContext.getPackageName()).thenReturn("com.god.seep.base");
        System.out.println(mContext.getPackageName());
    }
}