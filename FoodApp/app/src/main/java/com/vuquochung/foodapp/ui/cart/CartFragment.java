package com.vuquochung.foodapp.ui.cart;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vuquochung.foodapp.Adapter.MyCartAdapter;
import com.vuquochung.foodapp.Callback.ILoadTimeFromFirebaseListener;
import com.vuquochung.foodapp.Callback.ISearchCategoryCallbackListener;
import com.vuquochung.foodapp.Common.Common;
import com.vuquochung.foodapp.Common.MySwipeHelper;
import com.vuquochung.foodapp.Database.CartDataSource;
import com.vuquochung.foodapp.Database.CartDatabase;
import com.vuquochung.foodapp.Database.CartItem;
import com.vuquochung.foodapp.Database.LocalCartDataSource;
import com.vuquochung.foodapp.EventBus.CounterCartEvent;
import com.vuquochung.foodapp.EventBus.HideFABCart;
import com.vuquochung.foodapp.EventBus.MenuItemBack;
import com.vuquochung.foodapp.EventBus.UpdateItemInCart;
import com.vuquochung.foodapp.Model.AddonModel;
import com.vuquochung.foodapp.Model.CategoryModel;
import com.vuquochung.foodapp.Model.DiscountModel;
import com.vuquochung.foodapp.Model.FCMSenData;
import com.vuquochung.foodapp.Model.FoodModel;
import com.vuquochung.foodapp.Model.OrderModel;
import com.vuquochung.foodapp.Model.RestaurantLocationModel;
import com.vuquochung.foodapp.Model.RestaurantModel;
import com.vuquochung.foodapp.Model.SizeModel;
import com.vuquochung.foodapp.R;
import com.vuquochung.foodapp.Remote.IFCMService;
import com.vuquochung.foodapp.Remote.RetrofitFCMClient;
import com.vuquochung.foodapp.ScanQRActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;


public class CartFragment extends Fragment implements ILoadTimeFromFirebaseListener, ISearchCategoryCallbackListener, TextWatcher {

    private static final int SCAN_QR_PERMISSION = 7171;
    private BottomSheetDialog addonBottomSheetDialog;
    private ChipGroup chip_group_addon,chip_group_user_selected_addon;
    private EditText edt_search;

    private ISearchCategoryCallbackListener searchFoodCallbackListener;

    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;
    private List<Place.Field> placeFields= Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private Parcelable recyclerViewState;
    private CartDataSource cartDataSource;
    private CartViewModel cartViewModel;

    ILoadTimeFromFirebaseListener listener;
    //LocationRequest locationRequest;
    com.google.android.gms.location.LocationRequest locationRequest;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location currentLocation;

    IFCMService ifcmService;

    @BindView(R.id.recycler_cart)
    RecyclerView recycler_cart;
    @BindView(R.id.txt_total_price)
    TextView txt_total_price;
    @BindView(R.id.txt_empty_cart)
    TextView txt_empty_cart;
    @BindView(R.id.group_place_holder)
    CardView group_place_holder;
    @BindView(R.id.edt_discount_code)
    EditText edt_discount_code;


    @OnClick(R.id.img_scan)
    void onScanQRCode(){
        startActivityForResult(new Intent(requireContext(), ScanQRActivity.class),SCAN_QR_PERMISSION);
    }

    @OnClick(R.id.img_check)
    void onApplyDiscount(){
        if(!TextUtils.isEmpty(edt_discount_code.getText().toString()))
        {
            AlertDialog dialog=new AlertDialog.Builder(requireContext())
                    .setCancelable(false)
                    .setMessage("Please wait...")
                    .create();
            dialog.show();
            final DatabaseReference offsetRef=FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
            final DatabaseReference discountRef=FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF)
                    .child(Common.currentRestaurant.getUid())
                    .child(Common.DISCOUNT);
            offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long offset=snapshot.getValue(Long.class);
                    long estimatedServerTimeMs=System.currentTimeMillis()+offset;
                    discountRef.child(edt_discount_code.getText().toString().toLowerCase())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if(snapshot.exists())
                                    {
                                        DiscountModel discountModel=snapshot.getValue(DiscountModel.class);
                                        discountModel.setKey(snapshot.getKey());
                                        if(discountModel.getUntilDate()<estimatedServerTimeMs)
                                        {
                                            dialog.dismiss();
                                            listener.onLoadTimeFailed("Discount code has been expried");
                                        }
                                        else
                                        {
                                            dialog.dismiss();
                                            Common.discountApply=discountModel;
                                            sumAllItemInCart();
                                        }
                                    }
                                    else
                                    {
                                        dialog.dismiss();
                                        listener.onLoadTimeFailed("Discount not valid");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    dialog.dismiss();
                                    listener.onLoadTimeFailed(error.getMessage());
                                }
                            });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    dialog.dismiss();
                    listener.onLoadTimeFailed(error.getMessage());
                }
            });
        }
    }
    @OnClick(R.id.btn_place_order)
    void onPlaceOrderClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("One more step!");

        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_place_order, null);


        EditText edt_comment = (EditText) view.findViewById(R.id.edt_comment);
        TextView txt_address = (TextView) view.findViewById(R.id.txt_address_detail);
        RadioButton rdi_home = (RadioButton) view.findViewById(R.id.rdi_home_address);
        RadioButton rdi_other_address = (RadioButton) view.findViewById(R.id.rdi_other_address);
        RadioButton rdi_ship_to_this = (RadioButton) view.findViewById(R.id.rdi_ship_this_address);
        RadioButton rdi_cod = (RadioButton) view.findViewById(R.id.rdi_cod);
        RadioButton rdi_braintree = (RadioButton) view.findViewById(R.id.rdi_braintree);
        places_fragment=(AutocompleteSupportFragment)getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.places_autocomplete_fragment);
        places_fragment.setPlaceFields(placeFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(getContext(),""+status.getStatusMessage(),Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                placeSelected=place;
                txt_address.setText(place.getAddress());
            }
        });

        txt_address.setText(Common.currentUser.getAddress());
        //M???c ?????nh ch???n home address, n??n s??? xu???t hi???n ?????a ch???

        rdi_home.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                txt_address.setText(Common.currentUser.getAddress());
                txt_address.setVisibility(View.VISIBLE);
                places_fragment.setHint(Common.currentUser.getAddress());

            }
        });
        rdi_other_address.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {

                txt_address.setVisibility(View.VISIBLE);

            }
        });
        rdi_ship_to_this.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                //Toast.makeText(getContext(), "Implement late with Google API", Toast.LENGTH_SHORT).show();
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                fusedLocationProviderClient.getLastLocation().addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        txt_address.setVisibility(View.GONE);
                    }
                }).addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        String coordinates=new StringBuilder()
                                .append(task.getResult().getLatitude())
                                .append("/")
                                .append(task.getResult().getLongitude()).toString();

                        Single<String> singleAddress = Single.just(getAddressFromLatLng(task.getResult().getLatitude(),
                                task.getResult().getLongitude()));

                        Disposable disposable = singleAddress.subscribeWith(new DisposableSingleObserver<String>() {
                            @Override
                            public void onSuccess(String s) {
                                txt_address.setText(s);
                                txt_address.setVisibility(View.VISIBLE);
                                places_fragment.setHint(s);
                            }

                            @Override
                            public void onError(Throwable e) {
                                txt_address.setText(e.getMessage());
                                txt_address.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                });
            }
        });

        builder.setView(view);
        builder.setNegativeButton("NO", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        }).setPositiveButton("YES", (dialogInterface, i) -> {
            dialogInterface.dismiss();
            if(rdi_cod.isChecked()) {
                paymentCOD(txt_address.getText().toString(),txt_address.getText().toString());
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialogInterface -> {
            if(places_fragment != null)
                getActivity().getSupportFragmentManager()
                        .beginTransaction().remove(places_fragment)
                        .commit();
        });
        dialog.show();
    }

    private void paymentCOD(String address, String comment) {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentRestaurant.getUid())
                .child(Common.LOCATION_REF)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists())
                        {
                            RestaurantLocationModel location=snapshot.getValue(RestaurantLocationModel.class);
                            applyShippingCostByLocation(location,address,comment);
                        }
                        else
                            applyShippingCostByLocation(null,address,comment);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(),""+error.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void applyShippingCostByLocation(RestaurantLocationModel location,String address,String comment) {
        compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cartItems -> {
                    //when we have all cartItems, we will get total price
                    cartDataSource.sumPriceInCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleObserver<Double>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onSuccess(Double totalPrice) {
                                    double finalPrice = totalPrice;
                                    OrderModel orderModel = new OrderModel();
                                    orderModel.setUseId(Common.currentUser.getUid());
                                    orderModel.setUserName(Common.currentUser.getName());
                                    orderModel.setUserPhone(Common.currentUser.getPhone());
                                    orderModel.setShippingAddress(address);
                                    orderModel.setComment(comment);
                                    if(currentLocation != null) {
                                        orderModel.setLat(currentLocation.getLatitude());
                                        orderModel.setLng(currentLocation.getLongitude());
                                        if(location!=null)
                                        {
                                            Location orderLocation=new Location("");
                                            orderLocation.setLatitude(currentLocation.getLatitude());
                                            orderLocation.setLongitude(currentLocation.getLongitude());

                                            Location restaurantLocation=new Location("");
                                            restaurantLocation.setLatitude(location.getLat());
                                            restaurantLocation.setLongitude(location.getLng());

                                            float distance=orderLocation.distanceTo(restaurantLocation)/1000;
                                            Log.d("QUANGDUONG",distance+"");
                                            if(distance*Common.SHIPPING_COST_PER_KM > Common.MAX_SHIPPING_COST)
                                                orderModel.setShippingCost(Common.MAX_SHIPPING_COST);
                                            else
                                                orderModel.setShippingCost(distance*Common.SHIPPING_COST_PER_KM);
                                        }
                                        else
                                            orderModel.setShippingCost(0);
                                        Log.d("tienQUANGDUONG",orderModel.getShippingCost()+"");

                                    }
                                    else {
                                        orderModel.setLat(-0.1f);
                                        orderModel.setLng(-0.1f);

                                        orderModel.setShippingCost(Common.MAX_SHIPPING_COST);
                                    }

                                    orderModel.setCartItemList(cartItems);
                                    orderModel.setTotalPayment(totalPrice);
                                    if(Common.discountApply!=null)
                                        orderModel.setDiscount(Common.discountApply.getPercent());
                                    else
                                        orderModel.setDiscount(0);
                                    orderModel.setFinalPayment(finalPrice);
                                    orderModel.setCod(true);
                                    orderModel.setTransactionId("Cash On Delivery");

                                    //Submit this orderModel to FireBase
                                    syncLocalTimeWithGlobalTime(orderModel);
                                }


                                @Override
                                public void onError(Throwable e) {
                                    if(!e.getMessage().contains("Query returned empty result set"))
                                        Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });

                }, throwable -> {
                    Toast.makeText(getContext(), ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    private void syncLocalTimeWithGlobalTime(OrderModel orderModel) {
        AlertDialog dialog=new AlertDialog.Builder(requireContext())
                .setTitle("Shipping cost")
                .setMessage(new StringBuilder("We will take ")
                .append(Math.round(orderModel.getShippingCost()*100.0)/100.0)
                .append("$ for shipping your order\nYour order total payment is:")
                .append(Math.round((orderModel.getFinalPayment()+orderModel.getShippingCost())*100.0)/100.0)
                .append("$").toString())
                .setNegativeButton("NO", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })
                .setPositiveButton("YES", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    final DatabaseReference offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
                    offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            long offset = snapshot.getValue(Long.class);
                            long estimatedServerTimeMs = System.currentTimeMillis() +offset;
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
                            Date resultDate = new Date(estimatedServerTimeMs);
                            Log.d("TEST_DATE",""+sdf.format(resultDate));
                            listener.onLoadTimeSuccess(orderModel,estimatedServerTimeMs);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            listener.onLoadTimeFailed(error.getMessage());
                        }
                    });
                }).create();
        dialog.show();
    }

    private void writeOrderToFirebase(OrderModel orderModel) {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentRestaurant.getUid())
                .child(Common.ORDER_REF)
                .child(Common.createOrderNumber())
                .setValue(orderModel)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnCompleteListener(task -> {
                    //write success
                    cartDataSource.cleanCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleObserver<Integer>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onSuccess(Integer integer) {
                                    Map<String,String> notiData=new HashMap<>();
                                    notiData.put(Common.NOTI_TITLE,"New Order");
                                    notiData.put(Common.NOTI_CONTENT,"You have order from "+Common.currentUser.getPhone());

                                    FCMSenData senData=new FCMSenData(Common.createTopicOrder(),notiData);
                                    compositeDisposable.add(ifcmService.sendNotification(senData)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(fcmResponse -> {
                                                Toast.makeText(getContext(), "Order placed successfully", Toast.LENGTH_SHORT).show();
                                                EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                            }, throwable -> {
                                                Toast.makeText(getContext(), "Order was sent but failure to send notification", Toast.LENGTH_SHORT).show();
                                                EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                            }));

                                }

                                @Override
                                public void onError(Throwable e) {
                                    Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private String getAddressFromLatLng(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(getContext(),Locale.getDefault());
        String result="";
        try {
            List<Address> addressList = geocoder.getFromLocation(latitude,longitude,1);
            if(addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0);
                StringBuilder sb = new StringBuilder(address.getAddressLine(0));
                result = sb.toString();
            }
            else
                result = "Address not found";
        }
        catch (IOException e) {
            e.printStackTrace();
            result = e.getMessage();
        }
        return result;
    }

    private MyCartAdapter adapter;
    private Unbinder unbinder;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        cartViewModel =
                new ViewModelProvider(this).get(CartViewModel.class);
        listener = this;
        //binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        //View root = binding.getRoot();
        View root = inflater.inflate(R.layout.fragment_cart, container, false);
        ifcmService= RetrofitFCMClient.getInstance().create(IFCMService.class);
        cartViewModel.initCartDataSource(getContext());
        cartViewModel.getMutableLiveDataCartItems().observe(getViewLifecycleOwner(), new Observer<List<CartItem>>() {
            @Override
            public void onChanged(List<CartItem> cartItems) {
                if (cartItems == null || cartItems.isEmpty()) {
                    recycler_cart.setVisibility(View.GONE);
                    group_place_holder.setVisibility(View.GONE);
                    txt_empty_cart.setVisibility(View.VISIBLE);
                } else {
                    recycler_cart.setVisibility(View.VISIBLE);
                    group_place_holder.setVisibility(View.VISIBLE);
                    txt_empty_cart.setVisibility(View.GONE);

                    adapter = new MyCartAdapter(getContext(), cartItems);
                    recycler_cart.setAdapter(adapter);
                }
            }
        });
        unbinder = ButterKnife.bind(this, root);
        initViews();
        initLocation();
        return root;
    }

    private void initLocation() {
        buildLocationRequest();
        buildLocationCallBack();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
            }
        };
    }

    private void buildLocationRequest() {
       /*locationRequest = new LocationRequest.Builder(long intervalMillis);
       locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
       locationRequest.setInterval(5000);
       locationRequest.setFastestInterval(3000);
       locationRequest.setSmallestDisplacement(10f);*/
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }

    private void initViews() {
        searchFoodCallbackListener=this;
        initPlaceClient();
        setHasOptionsMenu(true);
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());
        EventBus.getDefault().postSticky(new HideFABCart(true));
        recycler_cart.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_cart.setLayoutManager(layoutManager);
        recycler_cart.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));
        //calculateTotalPrice();
        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_cart, 200) {
            @Override
            public void instantialMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Delete", 30, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                    CartItem cartItem = adapter.getItemAtPosition(pos);
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
                                    Toast.makeText(getContext(), "Delete item from Cart successful!", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }));


                buf.add(new MyButton(getContext(), "Update", 30, 0, Color.parseColor("#5D4037"),
                        pos -> {

                    CartItem cartItem=adapter.getItemAtPosition(pos);
                    FirebaseDatabase.getInstance()
                            .getReference(Common.RESTAURANT_REF)
                            .child(Common.currentRestaurant.getUid())
                            .child(Common.CATEGORY_REF)
                            .child(cartItem.getCategoryId())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {

                                    if(snapshot.exists())
                                    {
                                        CategoryModel categoryModel=snapshot.getValue(CategoryModel.class);
                                        searchFoodCallbackListener.onSearchCategoryFound(categoryModel,cartItem);
                                    }
                                    else
                                    {
                                        searchFoodCallbackListener.onSearchCategoryNotFound("Food not found");

                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    searchFoodCallbackListener.onSearchCategoryNotFound(error.getMessage());
                                }
                            });

                        }));
            }
        };
        sumAllItemInCart();

        //addon
        addonBottomSheetDialog=new BottomSheetDialog(getContext(),R.style.DialogStyle);
        View layout_addon_display=getLayoutInflater().inflate(R.layout.layout_addon_display,null);
        chip_group_addon=(ChipGroup) layout_addon_display.findViewById(R.id.chip_group_addon);
        edt_search=(EditText) layout_addon_display.findViewById(R.id.edt_search);
        addonBottomSheetDialog.setContentView(layout_addon_display);

        addonBottomSheetDialog.setOnDismissListener(dialogInterface -> {
            displayUserSelectedAddon(chip_group_user_selected_addon);
            calculateTotalPrice();
        });
    }

    private void displayUserSelectedAddon(ChipGroup chip_group_user_selected_addon) {
        if(Common.selectedFood.getUserSelectedAddon() != null && Common.selectedFood.getUserSelectedAddon().size() > 0)
        {
            chip_group_user_selected_addon.removeAllViews();
            for (AddonModel addonModel:Common.selectedFood.getUserSelectedAddon())
            {
                Chip chip=(Chip) getLayoutInflater().inflate(R.layout.layout_chip_with_delete_icon,null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+$")
                        .append(addonModel.getPrice()).append(")"));
                chip.setOnCheckedChangeListener((compoundButton, b) -> {
                    if(b)
                    {
                        if(Common.selectedFood.getUserSelectedAddon() == null)
                            Common.selectedFood.setUserSelectedAddon(new ArrayList<>());
                        Common.selectedFood.getUserSelectedAddon().add(addonModel);
                    }
                });
                chip_group_user_selected_addon.addView(chip);
            }
        }
        else
        {
            chip_group_user_selected_addon.removeAllViews();
        }
    }

    private void initPlaceClient() {
        Places.initialize(getContext(),getString(R.string.google_maps_api));
        placesClient=Places.createClient(getContext());
    }

    private void sumAllItemInCart() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double aDouble) {
                        if(Common.discountApply!=null)
                        {
                            aDouble=aDouble-(aDouble*Common.discountApply.getPercent()/100);
                            txt_total_price.setText(new StringBuilder("Total: $").append(Common.formatPrice(aDouble))
                            .append("(-")
                            .append(Common.discountApply.getPercent())
                            .append("%)"));
                        }
                        else
                        {
                            txt_total_price.setText(new StringBuilder("Total: $").append(Common.formatPrice(aDouble)));

                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (!e.getMessage().contains("Query returned empty"))
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT);
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
        inflater.inflate(R.menu.cart_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_cart) {
            cartDataSource.cleanCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            Toast.makeText(getContext(), "Clear Cart Success", Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(),   ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().removeAllStickyEvents();
        EventBus.getDefault().postSticky(new HideFABCart(false));
        EventBus.getDefault().postSticky(new CounterCartEvent(false));
        cartViewModel.onStop();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fusedLocationProviderClient != null) {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }

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
        cartDataSource.sumPriceInCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
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
                        if(!e.getMessage().contains("Query returned empty result set"))
                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onLoadTimeSuccess(OrderModel orderModel, long estimateTimeInMs) {
        orderModel.setCreateDate(estimateTimeInMs);
        orderModel.setOrderStatus(0);
        writeOrderToFirebase(orderModel);
       // syncLocalTimeWithGlobalTime(orderModel);
    }

    @Override
    public void onLoadOnlyTimeSuccess(long estimateTimeInMs) {
        //Do nothing
    }

    @Override
    public void onLoadTimeFailed(String message) {
        Toast.makeText(getContext(), ""+message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new MenuItemBack());
        super.onDestroy();
    }


    @Override
    public void onSearchCategoryFound(CategoryModel categoryModel,CartItem cartItem) {
        FoodModel foodModel=Common.findFoodInListById(categoryModel,cartItem.getFoodId());
        if(foodModel != null)
        {
            showUpdateDialog(cartItem,foodModel);
        }
        else
            Toast.makeText(getContext(), "Food Id not found", Toast.LENGTH_SHORT).show();
        
    }

    private void showUpdateDialog(CartItem cartItem, FoodModel foodModel) {
        Common.selectedFood=foodModel;
        AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
        View itemView=LayoutInflater.from(getContext()).inflate(R.layout.layout_dialog_update_cart,null);
        builder.setView(itemView);


        //view
        Button btn_ok=itemView.findViewById(R.id.btn_ok);
        Button btn_cancel=itemView.findViewById(R.id.btn_cancel);

        RadioGroup rdi_group_size=itemView.findViewById(R.id.rdi_group_size);
        chip_group_user_selected_addon=(ChipGroup)itemView.findViewById(R.id.chip_group_user_selected_addon);
        ImageView img_add_on=itemView.findViewById(R.id.img_add_addon);
        img_add_on.setOnClickListener(view -> {
           if(foodModel.getAddon() != null)
           {
               displayAddonList();
               addonBottomSheetDialog.show();
           }
        });
        //size
        if(foodModel.getSize() != null)
        {
            for (SizeModel sizeModel : foodModel.getSize())
            {
                RadioButton radioButton=new RadioButton(getContext());
                radioButton.setOnCheckedChangeListener((compoundButton, b) -> {
                    if(b)
                        Common.selectedFood.setUserSelectedSize(sizeModel);
                    calculateTotalPrice();
                });

                LinearLayout.LayoutParams params =new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1.0f);
                radioButton.setLayoutParams(params);
                radioButton.setText(sizeModel.getName());
                radioButton.setTag(sizeModel.getPrice());

                rdi_group_size.addView(radioButton);
            }
            if(rdi_group_size.getChildCount() > 0)
            {
                RadioButton radioButton=(RadioButton)rdi_group_size.getChildAt(0); //get first radio button
                radioButton.setChecked(true); // set default at first radio button
            }
        }
        //Addon
        displayAlreadySelectedAddon(chip_group_user_selected_addon,cartItem);

        //show dialog
        AlertDialog dialog=builder.create();
        dialog.show();
        //custom dialog
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.CENTER);
        //events
        btn_ok.setOnClickListener(view -> {
            //first,delete item in cart
            cartDataSource.deleteCartItem(cartItem)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            //after that , update information and add new
                            //update price and info
                            if(Common.selectedFood.getUserSelectedAddon() != null)
                                cartItem.setFoodAddon(new Gson().toJson(Common.selectedFood.getUserSelectedAddon()));
                            else
                                cartItem.setFoodAddon("Default");
                            if(Common.selectedFood.getUserSelectedSize() != null)
                                cartItem.setFoodSize(new Gson().toJson(Common.selectedFood.getUserSelectedSize()));
                            else
                                cartItem.setFoodSize("Default");

                            cartItem.setFoodExtraPrice(Common.calculateExtraPrice(Common.selectedFood.getUserSelectedSize(),
                                    Common.selectedFood.getUserSelectedAddon()));
                            Log.d("DATACART",cartItem.getFoodName()+"-"+cartItem.getFoodSize());
                            //Insert new
                            compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(()->{
                                        Toast.makeText(getContext(),cartItem.getFoodName(),Toast.LENGTH_SHORT).show();
                                        EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                        calculateTotalPrice();
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), "Update cart success", Toast.LENGTH_SHORT).show();
                                        cartViewModel.initCartDataSource(getContext());
                                        cartViewModel.getMutableLiveDataCartItems().observe(getViewLifecycleOwner(), new Observer<List<CartItem>>() {
                                            @Override
                                            public void onChanged(List<CartItem> cartItems) {
                                                if (cartItems == null || cartItems.isEmpty()) {
                                                    recycler_cart.setVisibility(View.GONE);
                                                    group_place_holder.setVisibility(View.GONE);
                                                    txt_empty_cart.setVisibility(View.VISIBLE);
                                                } else {
                                                    recycler_cart.setVisibility(View.VISIBLE);
                                                    group_place_holder.setVisibility(View.VISIBLE);
                                                    txt_empty_cart.setVisibility(View.GONE);

                                                    adapter = new MyCartAdapter(getContext(), cartItems);
                                                    recycler_cart.setAdapter(adapter);
                                                }
                                            }
                                        });
                                    },throwable -> {
                                        Toast.makeText(getContext(), ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    })
                            );
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        btn_cancel.setOnClickListener(view -> {
           dialog.dismiss();
        });

    }

    private void displayAlreadySelectedAddon(ChipGroup chip_group_user_selected_addon, CartItem cartItem) {
        if(cartItem.getFoodAddon() != null && !cartItem.getFoodAddon().equals("Default"))
        {
            List<AddonModel> addonModels=new Gson().fromJson(
                    cartItem.getFoodAddon(),new TypeToken<List<AddonModel>>(){}.getType());
            Common.selectedFood.setUserSelectedAddon(addonModels);
            chip_group_user_selected_addon.removeAllViews();
            //add all view
            for (AddonModel addonModel:addonModels)
            {
                Chip chip=(Chip) getLayoutInflater().inflate(R.layout.layout_chip_with_delete_icon,null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+$")
                        .append(addonModel.getPrice()).append(")"));
                chip.setClickable(false);
                chip.setOnCloseIconClickListener(view -> {
                    chip_group_user_selected_addon.removeView(view);
                    Common.selectedFood.getUserSelectedAddon().remove(addonModel);
                    calculateTotalPrice();
                });
                chip_group_user_selected_addon.addView(chip);
            }
        }
    }

    private void displayAddonList() {
        if(Common.selectedFood.getAddon() != null &&  Common.selectedFood.getAddon().size() > 0)
        {
            chip_group_addon.clearCheck();
            chip_group_addon.removeAllViews();

            edt_search.addTextChangedListener(this);

            //add all view
            for (AddonModel addonModel:Common.selectedFood.getAddon())
            {
                Chip chip=(Chip) getLayoutInflater().inflate(R.layout.layout_addon_item,null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+$")
                        .append(addonModel.getPrice()).append(")"));
                chip.setOnCheckedChangeListener((compoundButton, b) -> {
                    if(b)
                    {
                        if(Common.selectedFood.getUserSelectedAddon() == null)
                            Common.selectedFood.setUserSelectedAddon(new ArrayList<>());
                        Common.selectedFood.getUserSelectedAddon().add(addonModel);
                    }
                });
                chip_group_addon.addView(chip);
            }
        }
    }

    @Override
    public void onSearchCategoryNotFound(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        chip_group_addon.clearCheck();
        chip_group_addon.removeAllViews();
        for (AddonModel addonModel : Common.selectedFood.getAddon())
        {
            if(addonModel.getName().toLowerCase().contains(charSequence.toString().toLowerCase()))
            {
                Chip chip=(Chip) getLayoutInflater().inflate(R.layout.layout_addon_item,null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+$")
                        .append(addonModel.getPrice()).append(")"));
                chip.setOnCheckedChangeListener((compoundButton, b) -> {
                    if(b)
                    {
                        if(Common.selectedFood.getUserSelectedAddon() == null)
                            Common.selectedFood.setUserSelectedAddon(new ArrayList<>());
                        Common.selectedFood.getUserSelectedAddon().add(addonModel);
                    }
                });
                chip_group_addon.addView(chip);
            }
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==SCAN_QR_PERMISSION)
        {
            if(resultCode== Activity.RESULT_OK)
            {
                edt_discount_code.setText(data.getStringExtra(Common.QR_CODE_TAG).toLowerCase());
            }
        }
    }
}