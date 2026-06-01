package ru.yandex.practicum.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;
import ru.yandex.practicum.model.PageableObject;
import ru.yandex.practicum.model.SortObject;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageProductDto {

    @Valid
    private List<ProductDto> content;

    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private int size;
    private int number;
    private int numberOfElements;
    private boolean empty;
    private SortObject sort;
    private PageableObject pageable;
}