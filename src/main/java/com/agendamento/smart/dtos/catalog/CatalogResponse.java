package com.agendamento.smart.dtos.catalog;

import java.util.List;

public record CatalogResponse<T>(List<T> content) {
}
