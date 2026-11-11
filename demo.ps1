$ErrorActionPreference = "Stop"
$api = "http://localhost:8080/api"
$n = Get-Random -Minimum 1000 -Maximum 9999
$sellerEmail = "seller$n@example.com"
$buyerEmail = "buyer$n@example.com"

function Send-Post($path, $body, $headers = @{}) {
    Invoke-RestMethod -Uri "$api$path" -Method Post -ContentType "application/json" -Headers $headers -Body ($body | ConvertTo-Json)
}

# Prints chosen fields immediately, so the output stays in order
function Show($obj, [string[]]$props) {
    ($obj | Select-Object $props | Format-List | Out-String).Trim() | Write-Host
}

Write-Host "`n1. Register a seller and a buyer" -ForegroundColor Cyan
$regSeller = Send-Post "/auth/register" @{ fullName = "Demo Seller"; email = $sellerEmail; phone = "08011111111"; password = "password123"; role = "SELLER" }
Show $regSeller fullName, email, role
Write-Host ""
$regBuyer = Send-Post "/auth/register" @{ fullName = "Demo Buyer"; email = $buyerEmail; phone = "08022222222"; password = "password123"; role = "BUYER" }
Show $regBuyer fullName, email, role

Write-Host "`n2. Logging in before the email is verified is refused" -ForegroundColor Cyan
try {
    Send-Post "/auth/login" @{ email = $buyerEmail; password = "password123" } | Out-Null
    throw "Login should have been refused"
} catch {
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode.value__ -eq 403) {
        Write-Host "Refused with status 403: the email has not been verified yet"
    } else {
        throw
    }
}

Write-Host "`n3. Verify the two demo accounts" -ForegroundColor Cyan
Write-Host "In real use the user clicks the link in their email. The demo accounts use reserved"
Write-Host "example.com addresses with no inbox, so they are marked verified in the database."
$js = "db.users.updateMany({ email: { `$in: ['$sellerEmail', '$buyerEmail'] } }, { `$set: { emailVerified: true } })"
mongosh urua --quiet --eval $js | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Could not update the database. Is mongosh installed and MongoDB running?" }
Write-Host "Both accounts verified"

Write-Host "`n4. Log in (each user gets a token)" -ForegroundColor Cyan
$seller = Send-Post "/auth/login" @{ email = $sellerEmail; password = "password123" }
$buyer = Send-Post "/auth/login" @{ email = $buyerEmail; password = "password123" }
$sh = @{ Authorization = "Bearer $($seller.token)" }
$bh = @{ Authorization = "Bearer $($buyer.token)" }
Write-Host ("Both logged in. Token starts with: " + $seller.token.Substring(0, 12) + "...")

Write-Host "`n5. Seller lists a product" -ForegroundColor Cyan
$product = Send-Post "/products" @{ name = "Ankara Tote Bag"; description = "Handmade Ankara fabric tote bag"; price = 12500; category = "Fashion"; stockQuantity = 10; state = "Lagos" } $sh
Show $product name, price, stockQuantity, sellerName

Write-Host "`n6. Anyone can search the catalog (no login needed)" -ForegroundColor Cyan
Write-Host ("Products matching 'tote': " + (Invoke-RestMethod -Uri "$api/products?q=tote" -Method Get).totalElements)

Write-Host "`n7. Buyer adds 2 to the cart" -ForegroundColor Cyan
$cart = Send-Post "/cart/items" @{ productId = $product.id; quantity = 2 } $bh
Show $cart totalItems, subtotal

Write-Host "`n8. Buyer checks out" -ForegroundColor Cyan
$checkout = Send-Post "/orders/checkout" @{ fullName = "Demo Buyer"; phone = "08022222222"; addressLine = "12 Allen Avenue"; city = "Ikeja"; state = "Lagos" } $bh
Show $checkout orderId, paymentReference, total
Write-Host "`nOpening Paystack in your browser. Choose 'Success', then click Pay." -ForegroundColor Yellow
Start-Process $checkout.paymentUrl
Read-Host "Press Enter here AFTER the payment completes"

Write-Host "`n9. Server verifies the payment directly with Paystack" -ForegroundColor Cyan
$order = Invoke-RestMethod -Uri "$api/orders/verify/$($checkout.paymentReference)" -Method Post -Headers $bh
Show $order id, status, total, paidAt
if ($order.status -ne "PAID") { throw "Payment was not completed (status: $($order.status))" }

Write-Host "`n10. Seller ships the order" -ForegroundColor Cyan
$shipped = Invoke-RestMethod -Uri "$api/seller/orders/$($order.id)/ship" -Method Put -Headers $sh
Show $shipped id, status

Write-Host "`n11. Buyer confirms delivery" -ForegroundColor Cyan
$delivered = Invoke-RestMethod -Uri "$api/orders/$($order.id)/confirm-delivery" -Method Post -Headers $bh
Show $delivered id, status

Write-Host "`nDone: PAID -> SHIPPED -> DELIVERED" -ForegroundColor Green