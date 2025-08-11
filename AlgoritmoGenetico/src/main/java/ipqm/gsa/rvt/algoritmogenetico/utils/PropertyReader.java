package ipqm.gsa.rvt.algoritmogenetico.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pablo Rangel <pablo.rangel@marinha.mil.br> editada por Pedro Magno.
 * @grupo LaFIA (Laboratório de Fusão e Inteligência Artificial Aplicada)
 */

public class PropertyReader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyReader.class);
    
    public static Properties getProp() {
        Properties props = new Properties();
        try{
            FileInputStream file = new FileInputStream(
                "rvt.properties");            
                props.load(file);
        }
        catch(IOException ex){
            LOGGER.error("Arquivo não encontrado. ", ex.getMessage());
        }
        
        return props;
    }
}
