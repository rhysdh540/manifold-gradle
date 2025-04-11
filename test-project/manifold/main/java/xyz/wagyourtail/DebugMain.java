package xyz.wagyourtail;

public class DebugMain {

    public static void main(String[] args) {
        #if DEBUG == "true"
            System.out.println("This is a debug build.");
        #else
            System.out.println("This is a release build.");
        #endif
    }

}
