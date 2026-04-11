package com.example.myapplication.data.common;

/**
 * Generic callback for asynchronous repository operations.
 * Defined outside of any repository so that UseCases and ViewModels
 * are not coupled to a specific repository implementation.
 */
public interface RepositoryCallback<T> {
    void onSuccess(T data);
    void onError(UiMessage error);
}
