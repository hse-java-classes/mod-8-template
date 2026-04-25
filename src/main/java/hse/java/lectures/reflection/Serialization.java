package hse.java.lectures.reflection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import java.io.*;

public class Serialization {

    static String userJson = """
                    {
                      "name": "Ivan",
                      "age": 15
                    }
            """;

    static void main() throws IOException, ClassNotFoundException {
        User user = new User("Ivan", 15);
        System.out.println(user);

//        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("object"));
//        oos.writeObject(user);

        User fromObject;
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("object"));
        fromObject = (User) ois.readObject();
        System.out.println(fromObject);

        Gson gson = new Gson();
        User gsonUser = gson.fromJson(userJson, User.class);
        System.out.println(gsonUser);

        ObjectMapper objectMapper = new ObjectMapper();
        User jacksonUser = objectMapper.readValue(userJson, User.class);
        System.out.println(jacksonUser);
    }
}
