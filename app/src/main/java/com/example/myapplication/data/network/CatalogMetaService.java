package com.example.myapplication.data.network;

import com.example.myapplication.data.model.DestinationResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface CatalogMetaService {

    @GET
    Call<List<DestinationResponse>> listDestinations(@Url String url);

    @GET
    Call<List<String>> listCategories(@Url String url);
}

