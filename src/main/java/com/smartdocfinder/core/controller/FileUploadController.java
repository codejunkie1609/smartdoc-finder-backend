import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {
    @PostMapping(path = "/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        System.out.println("file received!");
        if(file.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("file is empty or missing");
        }
        else{
            return ResponseEntity.status(HttpStatus.OK).body("File received successfully");
        }
    }
}