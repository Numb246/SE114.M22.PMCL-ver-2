package com.vuquochung.foodapp.ui.cart;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.vuquochung.foodapp.Common.Common;
import com.vuquochung.foodapp.Database.CartDataSource;
import com.vuquochung.foodapp.Database.CartDatabase;
import com.vuquochung.foodapp.Database.CartItem;
import com.vuquochung.foodapp.Database.LocalCartDataSource;
import com.vuquochung.foodapp.Model.FoodModel;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class CartViewModel extends ViewModel {

    private CompositeDisposable compositeDisposable;
    private CartDataSource cartDataSource;
    private MutableLiveData<List<CartItem>> mutableLiveDataCartItems;

    public CartViewModel() {
        compositeDisposable=new CompositeDisposable();
    }
    public void initCartDataSource(Context context)
    {
        cartDataSource=new LocalCartDataSource(CartDatabase.getInstance(context).cartDAO());

    }

    public void onStop(){
        compositeDisposable.clear();
    }

    public MutableLiveData<List<CartItem>> getMutableLiveDataCartItems() {
        if(mutableLiveDataCartItems == null)
            mutableLiveDataCartItems=new MutableLiveData<>();
        getAllCartItems();
        return mutableLiveDataCartItems;
    }

    private void getAllCartItems() {
        compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(cartItems -> {
            Log.e("IMGCart",cartItems.get(0).getFoodImage()+"-"+cartItems.get(0).getFoodName());
            mutableLiveDataCartItems.setValue(cartItems);
        },throwable -> {
            mutableLiveDataCartItems.setValue(null);
        }));
    }
}