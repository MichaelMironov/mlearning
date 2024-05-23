package ru.ifellow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.ifellow.service.AnomalyDetectorService;

@Controller
public class AnomalyDetectorController {

    @Autowired
    private AnomalyDetectorService anomalyDetectorService;

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
    public String train(@RequestParam("sourceType") String sourceType,
                        @RequestParam("fileType") String fileType,
                        @RequestParam("algorithm") String algorithm,
                        @RequestParam("file") MultipartFile file,
                        Model model) {
        String status = anomalyDetectorService.trainModel(sourceType, fileType, algorithm, file);
        model.addAttribute("message", status);
        return "result";
    }

    @PostMapping("/detect")
    public String detect(@RequestParam("testFile") MultipartFile testFile,
                         @RequestParam("anomalyMetric") String anomalyMetric,
                         Model model) {
        String status = anomalyDetectorService.detectAnomalies(testFile, anomalyMetric);
        model.addAttribute("detectionStatus", status);
        return "index";
    }
}