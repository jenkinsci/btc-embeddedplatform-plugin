package com.btc.ep.plugins.embeddedplatform.test;

import java.io.FileInputStream;

import org.yaml.snakeyaml.Yaml;

import com.btc.ep.plugins.embeddedplatform.model.TestConfig;

public class TestStuff_Thabo {

    public static void main(String[] args) throws Exception {
    	TestConfig testConfig = new Yaml().loadAs(new FileInputStream("E:/TestConfig.yaml"), TestConfig.class);
    	System.out.println(testConfig);
    }

}