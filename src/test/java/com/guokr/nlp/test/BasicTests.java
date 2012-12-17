package com.guokr.nlp.test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.guokr.nlp.__PKG__;

@RunWith(JUnit4.class)
public class BasicTests {

    @Test
    public void testSeg() throws Exception {
        __PKG__ pkg = __PKG__.INSTANCE;
        Method segMtd = pkg.localSegWrapper.getDeclaredMethod("segment", String.class);
        String segText = (String)segMtd.invoke(pkg.seg, "这是个测试");
        assertEquals("这 是 个 测试", segText);
    }

    @Test
    public void testNer() throws Exception {
        __PKG__ pkg = __PKG__.INSTANCE;
        Method nerMtd = pkg.localNerWrapper.getDeclaredMethod("recognize", String.class);
        String nerText = (String)nerMtd.invoke(pkg.ner, "这是个测试");
        assertEquals("这/O 是/O 个/O 测试/O", nerText);
    }

    @Test
    public void testTag() throws Exception {
        __PKG__ pkg = __PKG__.INSTANCE;
        Method tagMtd = pkg.localTagWrapper.getDeclaredMethod("tag", String.class);
        String tagText = (String)tagMtd.invoke(pkg.tag, "这是个测试");
        assertEquals("这#PN 是#VC 个#M 测试#NN", tagText);
    }

}
