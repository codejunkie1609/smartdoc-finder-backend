package com.smartdocfinder.core.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;



@Service
public class DocumentParserService {
    public static String detectMediaType(byte[] data) throws IOException, TikaException{
        Tika tika = new Tika();
        return tika.detect(data);
     
        
    }

    public static String extractContent(InputStream stream) throws IOException, TikaException{
        Tika tika = new Tika();
        String content = tika.parseToString(stream);
        return content;
    }
}
