package uk.gov.justice.digital.noms.hub.ports.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.justice.digital.noms.hub.domain.ContentItem;
import uk.gov.justice.digital.noms.hub.domain.MediaRepository;
import uk.gov.justice.digital.noms.hub.domain.MetadataRepository;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
public class AdminController {

    private final MetadataRepository metadataRepository;
    private final MediaRepository mediaRepository;

    public AdminController(MetadataRepository metadataRepository, MediaRepository mediaRepository) {
        this.metadataRepository = metadataRepository;
        this.mediaRepository = mediaRepository;
    }

    @PostMapping("/content-items")
    public ResponseEntity saveFileAndMetadata(@RequestParam("file") MultipartFile file,
                                              @RequestParam("metadata") String metadata,
                                              UriComponentsBuilder uriComponentsBuilder) throws IOException {

        logParameters(file, metadata);
        Map<String, Object> verifiedMetadata = validateMetadataIsWellFormedJson(metadata);

        String mediaUri = mediaRepository.save(file.getInputStream(), file.getOriginalFilename(), file.getSize());
        String id = metadataRepository.save(new ContentItem(mediaUri, file.getOriginalFilename(), verifiedMetadata));

        return new ResponseEntity<Void>(createLocationHeader(uriComponentsBuilder, id), HttpStatus.CREATED);
    }

    @GetMapping("/content-items")
    public @ResponseBody ContentItemsResponse findAll() {
        return new ContentItemsResponse(metadataRepository.findAll());
    }

    private void logParameters(MultipartFile file, String metadata) {
        log.info("filename: " + file.getOriginalFilename());
        log.info("file size: " + file.getSize());
        log.info("metadata: " + metadata);
    }

    private HttpHeaders createLocationHeader(UriComponentsBuilder uriComponentsBuilder, String id) {
        HttpHeaders headers = new HttpHeaders();
        UriComponents uriComponents = uriComponentsBuilder.path("/content-items/{id}").buildAndExpand(id);
        headers.setLocation(uriComponents.toUri());
        return headers;
    }

    private Map<String, Object> validateMetadataIsWellFormedJson(String metadata) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(metadata, new TypeReference<Map<String, Object>>() { });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
