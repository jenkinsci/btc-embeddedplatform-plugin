package com.btc.ep.plugins.embeddedplatform.util;

import java.util.HashMap;
import java.util.Map;

public class Util {

    //    public static void main(String[] args) throws InterruptedException {
    //
    //        System.out.println(ExecutionModeEnum.TOP_DOWN.name());
    //
    //                    ProgressPrinter pp = new ProgressPrinter(System.out);
    //                    for (int i = 1; i <= 100; i++) {
    //                        pp.progress(i);
    //                        Thread.sleep(10);
    //                    }
    //    }

    public static Map<Object, Object> toMap(Object... objects) {
        if (objects.length % 2 != 0) {
            throw new IllegalArgumentException(
                "Inconsistent number of key-value pairs (received " + objects.length + " items).");
        }
        Map<Object, Object> map = new HashMap<>();
        for (int i = 0; i < (objects.length - 1); i = i + 2) {
            map.put(objects[i], objects[i + 1]);
        }
        return map;
    }

}
