import java.net.URL;

public class Main {
    static {
        final URL url = Main.class
                .getResource("jni.dylib");
        System.load(url.getPath());
    }

    public static native String helloJni();

    public static void main(String[] args) {
        System.out.println(helloJni());
    }
}
