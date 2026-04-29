package interfaces;

import java.util.List;

public interface IService <T> {

    void add(T p);
    List<T> getAll();
    void delete(T p);
    void update(T p);
    //getby getByID ...

}
