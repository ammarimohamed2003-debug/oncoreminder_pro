package interfaces;

import java.util.List;

public interface IService<T> {
    void add(T t);
    List<T> getAll();
    void delete(T t);
    void update(T t);
}
