package com.rauldev.personalfinance.entry.web.category;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rauldev.personalfinance.application.usecase.CreateCategory;
import com.rauldev.personalfinance.application.usecase.ListCategories;
import com.rauldev.personalfinance.application.usecase.ListCategoriesQuery;
import com.rauldev.personalfinance.entry.security.CurrentUserProvider;
import com.rauldev.personalfinance.entry.web.common.IdResponse;

/**
 * Categories of the current user.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CreateCategory createCategory;
    private final ListCategories listCategories;
    private final CurrentUserProvider currentUserProvider;

    public CategoryController(
        CreateCategory createCategory,
        ListCategories listCategories,
        CurrentUserProvider currentUserProvider
    ) {
        this.createCategory = Objects.requireNonNull(createCategory, "Create category cannot be null");
        this.listCategories = Objects.requireNonNull(listCategories, "List categories cannot be null");
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "Current user provider cannot be null");
    }

    /**
     * Creates a category and answers {@code 201 Created} without a {@code Location} header: there
     * is no endpoint to read a single category yet.
     */
    @PostMapping
    public ResponseEntity<IdResponse> create(@RequestBody CreateCategoryRequest request) {
        UUID categoryId = createCategory.execute(request.toCommand(currentUserProvider.currentUserId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(categoryId));
    }

    /**
     * Lists all the current user's categories, active and inactive, ordered by name then id (case-sensitive, see
     * application.md → Query Rules; clients sort by locale for display). The body is a
     * bare JSON array (empty when the user has no categories): the list is small and unpaginated.
     */
    @GetMapping
    public List<CategoryResponse> list() {
        return listCategories.execute(new ListCategoriesQuery(currentUserProvider.currentUserId())).stream()
            .map(CategoryResponse::from)
            .toList();
    }
}
