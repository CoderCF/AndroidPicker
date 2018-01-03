# AndroidPicker
地址选择器，日期选择器

# 效果图

![地址选择器](/screenshots/address.gif)
![时间选择器](/screenshots/date.gif)

#使用步骤

1.添加Gradle依赖

```
compile 'com.codercf:androidpickerlibrary:1.0.1'
```

2.选择器使用

- 地址选择器

```
AddressPicker picker = new AddressPicker(MainActivity.this);

                picker.setAddressListener(new AddressPicker.OnAddressListener() {
                    @Override
                    public void onAddressSelected(String province, String city, String area) {
                        btn_address.setText(province+"-"+city+"-"+area);
                    }
                });

                picker.show();
```

- 日期选择器

```
 DatePicker picker = new DatePicker(MainActivity.this);

                picker.setDateListener(new DatePicker.OnDateCListener() {
                    @Override
                    public void onDateSelected(String province, String city, String area) {
                        btn_date.setText(province+"-"+city+"-"+area);
                    }
                });
                picker.show();
```

#thanks


[https://github.com/gzu-liyujiang/AndroidPicker](https://github.com/gzu-liyujiang/AndroidPicker)

[https://github.com/Bigkoo/Android-PickerView](https://github.com/Bigkoo/Android-PickerView)

[https://github.com/weidongjian/androidWheelView/](https://github.com/weidongjian/androidWheelView/)

[https://github.com/brucetoo/PickView](https://github.com/brucetoo/PickView)
