package com.chrionline.chrionline.server.services;


import com.chrionline.chrionline.server.data.JdbcRepository;
import com.chrionline.chrionline.server.data.models.Produit;

import com.chrionline.chrionline.server.repositories.ProduitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class ProduitService {
    private static final Logger logger = LoggerFactory.getLogger(ProduitService.class);

    private final ProduitRepository produitRepository;

    public ProduitService(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
        logger.info("ProduitService initialized");
    }


}
