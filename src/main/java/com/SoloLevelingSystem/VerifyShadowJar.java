package com.SoloLevelingSystem;

import com.moandjiezana.toml.Toml;

public class VerifyShadowJar {
    public static void main(String[] args) {
        try {
            Class.forName("com.moandjiezana.toml.Toml");
            System.out.println("Success: toml4j is present in the ShadowJar!");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: toml4j is NOT present in the ShadowJar!");
            e.printStackTrace();
            System.exit(1); // Exit with an error code
        }
    }
}
