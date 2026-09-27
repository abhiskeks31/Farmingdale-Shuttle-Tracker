package com.farmingdale.shuttle.service;

import com.farmingdale.shuttle.entity.Shuttle;
import com.farmingdale.shuttle.repository.ShuttleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShuttleService {

    private final ShuttleRepository shuttleRepository;

    public ShuttleService(ShuttleRepository shuttleRepository) {
        this.shuttleRepository = shuttleRepository;
    }

    public List<Shuttle> getAllShuttles() {
        return shuttleRepository.findAll();
    }
}