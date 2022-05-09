package com.vuquochung.foodapp.ui.cart;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vuquochung.foodapp.Adapter.MyCartAdapter;
import com.vuquochung.foodapp.Adapter.MyFoodListAdapter;
import com.vuquochung.foodapp.Common.Common;
import com.vuquochung.foodapp.Common.MySwipeHelper;
import com.vuquochung.foodapp.Database.CartDataSource;
import com.vuquochung.foodapp.Database.CartDatabase;
import com.vuquochung.foodapp.Database.CartItem;
import com.vuquochung.foodapp.Database.LocalCartDataSource;
import com.vuquochung.foodapp.EventBus.CounterCartEvent;
import com.vuquochung.foodapp.EventBus.HideFABCart;
import com.vuquochung.foodapp.EventBus.UpdateItemInCart;
import com.vuquochung.foodapp.Model.FoodModel;
import com.vuquochung.foodapp.R;
import com.vuquochung.foodapp.ui.foodlist.FoodListViewModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CartFragment extends Fragment {
    private Parcelable recyclerViewState;
    private CartDataSource cartDataSource;
    private CartViewModel cartViewModel;
    @BindView(R.id.recycler_cart)
    RecyclerView recycler_cart;
    @BindView(R.id.txt_total_price)
    TextView txt_total_price;
    @BindView(R.id.txt_empty_cart)
    TextView txt_empty_cart;
    @BindView(R.id.group_place_holder)
    CardView group_place_holder;

    @OnClick(R.id.btn_place_order)
    void onPlaceOrderClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("One more step!");

        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_place_order,null);

        EditText edt_address = (EditText)view.findViewById(R.id.edt_address);
        RadioButton rdi_home = (RadioButton)view.findViewById(R.id.rdi_home_address);
        RadioButton rdi_other_address = (RadioButton)view.findViewById(R.id.rdi_other_address);
        RadioButton rdi_ship_to_this = (RadioButton)view.findViewById(R.id.rdi_ship_this_address);
        RadioButton rdi_cod = (RadioButton)view.findViewById(R.id.rdi_cod);
        RadioButton rdi_braintree = (RadioButton)view.findViewById(R.id.rdi_braintree);

        edt_address.setText(Common.currentUser.getAddress());
        //Mặc định chọn home address, nên sẽ xuất hiện địa chỉ

        rdi_home.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b){
                edt_address.setText(Common.currentUser.getAddress());
            }
        });
        rdi_other_address.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b){
                edt_address.setText(""); // xóa thông tin
//                edt_address.setHint("Enter your address");
            }
        });
        rdi_ship_to_this.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b){
                Toast.makeText(getContext(), "Implement late with Google API", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(view);
        builder.setNegativeButton("NO", (dialogInterface, i) -> {
             dialogInterface.dismiss();
        }).setPositiveButton("YES", (dialogInterface, i) -> {
            Toast.makeText(getContext(), "Implement late!", Toast.LENGTH_SHORT).show();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private MyCartAdapter adapter;
    private Unbinder unbinder;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        cartViewModel =
                new ViewModelProvider(this).get(CartViewModel.class);

        //binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        //View root = binding.getRoot();
        View root=inflater.inflate(R.layout.fragment_cart,container,false);
        cartViewModel.initCartDataSource(getContext());
        cartViewModel.getMutableLiveDataCartItems().observe(getViewLifecycleOwner(), new Observer<List<CartItem>>() {
            @Override
            public void onChanged(List<CartItem> cartItems) {
                if(cartItems==null || cartItems.isEmpty() )
                {
                    recycler_cart.setVisibility(View.GONE);
                    group_place_holder.setVisibility(View.GONE);
                    txt_empty_cart.setVisibility(View.VISIBLE);
                }
                else
                {
                    recycler_cart.setVisibility(View.VISIBLE);
                    group_place_holder.setVisibility(View.VISIBLE);
                    txt_empty_cart.setVisibility(View.GONE);

                    adapter=new MyCartAdapter(getContext(),cartItems);
                    recycler_cart.setAdapter(adapter);
                }
            }
        });
        unbinder=ButterKnife.bind(this,root);
        initViews();
        return root;
    }

    private void initViews() {
        setHasOptionsMenu(true);
        cartDataSource=new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());
        EventBus.getDefault().postSticky(new HideFABCart(true));
        recycler_cart.setHasFixedSize(true);
        LinearLayoutManager layoutManager=new LinearLayoutManager(getContext());
        recycler_cart.setLayoutManager(layoutManager);
        recycler_cart.addItemDecoration(new DividerItemDecoration(getContext(),layoutManager.getOrientation()));
        //calculateTotalPrice();
        MySwipeHelper mySwipeHelper=new MySwipeHelper(getContext(),recycler_cart,200) {
            @Override
            public void instantialMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(),"Delete",30,0, Color.parseColor("#FF3C30"),pos -> {
                    CartItem cartItem=adapter.getItemAtPosition(pos);
                    cartDataSource.deleteCartItem(cartItem)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleObserver<Integer>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onSuccess(Integer integer) {
                                    adapter.notifyItemRemoved(pos);
                                    sumAllItemInCart();
                                    EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                    Toast.makeText(getContext(),"Delete item from Cart successful!",Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Toast.makeText(getContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show();
                                }
                            });
                }));
            }
        };
        sumAllItemInCart();
    }

    private void sumAllItemInCart() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double aDouble) {
                        txt_total_price.setText(new StringBuilder("Total: $").append(Common.formatPrice(aDouble)));

                    }

                    @Override
                    public void onError(Throwable e) {
                        if(!e.getMessage().contains("Query returned empty"))
                            Toast.makeText(getContext(),""+e.getMessage(),Toast.LENGTH_SHORT);
                    }
                });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(false);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.cart_menu,menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.action_clear_cart)
        {
            cartDataSource.cleanCart(Common.currentUser.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            Toast.makeText(getContext(),"Clear Cart Success",Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().postSticky(new HideFABCart(false));
        cartViewModel.onStop();
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void onUpdateItemInCartEvent(UpdateItemInCart event)
    {
        Log.e("EEEEE","OK");
        if(event.getCartItem()!=null)
        {
            recyclerViewState=recycler_cart.getLayoutManager().onSaveInstanceState();
            cartDataSource.updateCartItems(event.getCartItem())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            calculateTotalPrice();
                            recycler_cart.getLayoutManager().onRestoreInstanceState(recyclerViewState);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(),"[UPDATE CART]"+e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void calculateTotalPrice() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double price) {
                        txt_total_price.setText(new StringBuilder("Total: $").append(Common.formatPrice(price)));
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getContext(),"[SUM CART]"+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}