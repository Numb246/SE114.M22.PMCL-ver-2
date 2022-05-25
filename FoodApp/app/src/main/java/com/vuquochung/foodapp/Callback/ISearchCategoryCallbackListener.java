package com.vuquochung.foodapp.Callback;

import com.vuquochung.foodapp.Database.CartItem;
import com.vuquochung.foodapp.Model.CategoryModel;
import com.vuquochung.foodapp.Model.FoodModel;

public interface ISearchCategoryCallbackListener {
    void onSearchCategoryFound(CategoryModel categoryModel, CartItem cartItem);
    void onSearchCategoryNotFound(String message);
}
