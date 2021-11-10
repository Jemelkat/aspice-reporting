package com.aspicereporting.service;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.User;
import com.aspicereporting.repository.SourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SourceService {

    @Autowired
    SourceRepository sourceRepository;

    public void storeFileAsSource(MultipartFile file, User user) {
        Source source = new Source();
        source.setSourceName(file.getOriginalFilename());
        source.setUser(user);

        sourceRepository.save(source);
    }
}
