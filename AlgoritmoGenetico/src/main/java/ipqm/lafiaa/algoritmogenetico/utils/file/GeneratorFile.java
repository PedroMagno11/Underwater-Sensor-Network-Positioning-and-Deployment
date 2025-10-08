package ipqm.lafiaa.algoritmogenetico.utils.file;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class GeneratorFile<T> {

    private static GeneratorFile instance = null;
    private final Deque<T> buffer;
    private final ReentrantLock lock;
    private final ObjectMapper mapper;

    private GeneratorFile(int size) {
        mapper = new ObjectMapper();
        buffer = new ArrayDeque<>(size);
        lock = new ReentrantLock();
    }

    public static GeneratorFile getInstance(int size) {
        if(instance == null){
            instance = new GeneratorFile(size);
        }
        return instance;
    }

    public void registrar(T obj){
        lock.lock();
        buffer.addLast(obj);
        lock.unlock();
    }

    public void salvar(String directory, String filename) throws IOException {
        List<T> snapshot;
        lock.lock();
        snapshot = new ArrayList<>(buffer);
        lock.unlock();

        String dir = System.getProperty("user.dir");
        Path path = Path.of(dir, directory);
        if(!Files.exists(path)){
            Files.createDirectories(path);
        }
        File file = new File(path.toString(), filename + ".json");
        if(file.exists()){
            file.delete();
        }
        file.createNewFile();

        try(FileWriter fw = new FileWriter(file)){
            fw.write(mapper.writeValueAsString(snapshot));
//            for(T obj : snapshot){
//                fw.write(mapper.writeValueAsString(obj));
//                fw.write(",\n");
//            }
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
