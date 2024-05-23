package ru.ifellow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.ifellow.service.AnomalyDetectorService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@Controller
public class AnomalyDetectorController {

    private final AnomalyDetectorService anomalyDetectorService;

    @Autowired
    public AnomalyDetectorController(AnomalyDetectorService anomalyDetectorService) {
        this.anomalyDetectorService = anomalyDetectorService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("sourceTypes", new String[]{"File", "Kafka"});
        model.addAttribute("fileTypes", new String[]{"csv", "json"});
        model.addAttribute("algorithms", new String[]{"KMeans", "BisectingKMeans"});
        model.addAttribute("anomalyMetrics", new String[]{"Manhattan", "Euclidean"});
        model.addAttribute("resultOutputTypes", new String[]{"File", "Kafka"});
        return "index";
    }

    @PostMapping("/train")
    public String train(@RequestParam("file") MultipartFile file, Model model) {
        String status = anomalyDetectorService.trainModel("File", "csv", "KMeans", file);
        model.addAttribute("trainingStatus", status);
        return "index";
    }

    @PostMapping("/detect")
    public String detect(@RequestParam("file") MultipartFile testFile, @RequestParam("anomalyMetric") String anomalyMetric, Model model) {
        String status = anomalyDetectorService.detectAnomalies(testFile, anomalyMetric);
        model.addAttribute("detectionStatus", status);
        return "index";
    }

    @GetMapping("/downloadModel")
    public ResponseEntity<InputStreamResource> downloadModel(@RequestParam("path") String path) {
        try {
            File file = new File(path);
            System.out.println("Attempting to download file from path: " + path);
            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(file.length())
                    .body(resource);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body(null);
        }
    }
}