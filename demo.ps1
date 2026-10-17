$ErrorActionPreference = "Stop"
$api = "http://localhost:8080/api"
$n = Get-Random -Minimum 1000 -Maximum 9999

function Send-Post($path, $body, $headers = @{}) {
    Invoke-RestMethod -Uri "$api$path" -Method Post -ContentType "application/json" -Headers $headers -Body ($body | ConvertTo-Json)
}

# Prints chosen fields immediately, so the output stays in order
function Show($obj, [string[]]$props) {
    ($obj | Select-Object $props | Format-List | Out-String).Trim() | Write-Host
}

Write-Host "`n1. Register a seller and a buyer" -ForegroundColor Cyan
$regSeller = Send-Post "/auth/register" @{ fullName = "Demo Seller"; email = "seller$n@test.com"; phone = "08011111111"; password = "password123"; role = "SELLER" }
Show $regSeller fullName, email, role
Write-Host ""
$regBuyer = Send-Post "/auth/register" @{ fullName = "Demo Buyer"; email = "buyer$n@test.com"; phone = "08022222222"; password = "password123"; role = "BUYER" }
Show $regBuyer fullName, email, role

Write-Host "`n2. Log in (each user gets a token)" -ForegroundColor Cyan
$seller = Send-Post "/auth/login" @{ email = "seller$n@test.com"; password = "password123" }
$buyer = Send-Post "/auth/login" @{ email = "buyer$n@test.com"; password = "password123" }
$sh = @{ Authorization = "Bearer $($seller.token)" }
$bh = @{ Authorization = "Bearer $($buyer.token)" }
Write-Host ("Both logged in. Token starts with: " + $seller.token.Substring(0, 12) + "...")

Write-Host "`n3. Seller lists a product" -ForegroundColor Cyan
$product = Send-Post "/products" @{ name = "Ankara Tote Bag"; description = "Handmade Ankara fabric tote bag"; price = 12500; category = "Fashion"; stockQuantity = 10; state = "Lagos" } $sh
Show $product name, price, stockQuantity, sellerName

Write-Host "`n4. Anyone can search the catalog (no login needed)" -ForegroundColor Cyan
Write-Host ("Products matching 'tote': " + (Invoke-RestMethod -Uri "$api/products?q=tote" -Method Get).totalElements)

Write-Host "`n5. Buyer adds 2 to the cart" -ForegroundColor Cyan
$cart = Send-Post "/cart/items" @{ productId = $product.id; quantity = 2 } $bh
Show $cart totalItems, subtotal

Write-Host "`n6. Buyer checks out" -ForegroundColor Cyan
$checkout = Send-Post "/orders/checkout" @{ fullName = "Demo Buyer"; phone = "08022222222"; addressLine = "12 Allen Avenue"; city = "Ikeja"; state = "Lagos" } $bh
Show $checkout orderId, paymentReference, total
Write-Host "`nOpening Paystack in your browser. Choose 'Success', then click Pay." -ForegroundColor Yellow
Start-Process $checkout.paymentUrl
Read-Host "Press Enter here AFTER the payment completes"

Write-Host "`n7. Server verifies the payment directly with Paystack" -ForegroundColor Cyan
$order = Invoke-RestMethod -Uri "$api/orders/verify/$($checkout.paymentReference)" -Method Post -Headers $bh
Show $order id, status, total, paidAt
if ($order.status -ne "PAID") { throw "Payment was not completed (status: $($order.status))" }

Write-Host "`n8. Seller ships the order" -ForegroundColor Cyan
$shipped = Invoke-RestMethod -Uri "$api/seller/orders/$($order.id)/ship" -Method Put -Headers $sh
Show $shipped id, status

Write-Host "`n9. Buyer confirms delivery" -ForegroundColor Cyan
$delivered = Invoke-RestMethod -Uri "$api/orders/$($order.id)/confirm-delivery" -Method Post -Headers $bh
Show $delivered id, status

Write-Host "`nDone: PAID -> SHIPPED -> DELIVERED" -ForegroundColor Green