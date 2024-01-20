
package com.example.real_heartrate;


        import com.google.firebase.database.Exclude;
        import com.google.firebase.database.IgnoreExtraProperties;

   //     import org.checkerframework.checker.units.qual.C;

        import java.util.HashMap;
        import java.util.Map;

@IgnoreExtraProperties
public class Post {

    public String hum;
    public String tmp;
    public String tof;

    public Map<String, Boolean> stars = new HashMap<>();

    public Post() {
        // Default constructor required for calls to DataSnapshot.getValue(Post.class)
    }

    public Post(String hum, String tmp, String tof) {
        this.hum = hum;
        this.tmp = tmp;
        this.tof = tof;

    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", hum);
        result.put("author", tmp);
        result.put("title", tof);


        return result;
    }
}

