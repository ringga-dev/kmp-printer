using System.Globalization;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.GenericAttributeProfile;
using Windows.Storage.Streams;

namespace KmpPrinter.BleBridge.Windows;

internal static class Program
{
    private static async Task<int> Main(string[] args)
    {
        if (args.Contains("--version"))
        {
            Console.WriteLine("kmp-printer-ble-windows 1.0.0");
            return 0;
        }

        var options = Options.Parse(args);
        if (options == null)
        {
            Console.Error.WriteLine("Usage: kmp-printer-ble-windows --connect <mac> --service <uuid> --characteristic <uuid>");
            return 2;
        }

        await using var session = await BleSession.Open(options);
        if (session == null)
        {
            Console.Error.WriteLine("Unable to open BLE session.");
            return 3;
        }

        Console.WriteLine("READY");
        string? line;
        while ((line = Console.ReadLine()) != null)
        {
            line = line.Trim();
            if (line.Equals("QUIT", StringComparison.OrdinalIgnoreCase)) break;
            if (line.Length == 0) continue;

            var payload = Hex.Decode(line);
            if (payload.Length == 0)
            {
                Console.WriteLine("ERR invalid-hex");
                continue;
            }

            var ok = await session.Write(payload);
            Console.WriteLine(ok ? "OK" : "ERR write-failed");
        }

        return 0;
    }
}

internal sealed record Options(ulong Address, Guid ServiceUuid, Guid CharacteristicUuid)
{
    public static Options? Parse(string[] args)
    {
        string? address = ValueAfter(args, "--connect");
        string? service = ValueAfter(args, "--service");
        string? characteristic = ValueAfter(args, "--characteristic");
        if (address == null || service == null || characteristic == null) return null;
        if (!Guid.TryParse(service, out var serviceUuid)) return null;
        if (!Guid.TryParse(characteristic, out var characteristicUuid)) return null;

        var normalizedAddress = address.Replace(":", "").Replace("-", "");
        if (!ulong.TryParse(normalizedAddress, NumberStyles.HexNumber, CultureInfo.InvariantCulture, out var bluetoothAddress))
        {
            return null;
        }

        return new Options(bluetoothAddress, serviceUuid, characteristicUuid);
    }

    private static string? ValueAfter(string[] args, string key)
    {
        var index = Array.IndexOf(args, key);
        return index >= 0 && index + 1 < args.Length ? args[index + 1] : null;
    }
}

internal sealed class BleSession : IAsyncDisposable
{
    private readonly BluetoothLEDevice _device;
    private readonly GattDeviceService _service;
    private readonly GattCharacteristic _characteristic;

    private BleSession(BluetoothLEDevice device, GattDeviceService service, GattCharacteristic characteristic)
    {
        _device = device;
        _service = service;
        _characteristic = characteristic;
    }

    public static async Task<BleSession?> Open(Options options)
    {
        var device = await BluetoothLEDevice.FromBluetoothAddressAsync(options.Address);
        if (device == null) return null;

        var servicesResult = await device.GetGattServicesForUuidAsync(options.ServiceUuid, BluetoothCacheMode.Uncached);
        if (servicesResult.Status != GattCommunicationStatus.Success || servicesResult.Services.Count == 0)
        {
            device.Dispose();
            return null;
        }

        var service = servicesResult.Services[0];
        var characteristicsResult = await service.GetCharacteristicsForUuidAsync(options.CharacteristicUuid, BluetoothCacheMode.Uncached);
        if (characteristicsResult.Status != GattCommunicationStatus.Success || characteristicsResult.Characteristics.Count == 0)
        {
            service.Dispose();
            device.Dispose();
            return null;
        }

        return new BleSession(device, service, characteristicsResult.Characteristics[0]);
    }

    public async Task<bool> Write(byte[] payload)
    {
        using var writer = new DataWriter();
        writer.WriteBytes(payload);
        var writeType = _characteristic.CharacteristicProperties.HasFlag(GattCharacteristicProperties.WriteWithoutResponse)
            ? GattWriteOption.WriteWithoutResponse
            : GattWriteOption.WriteWithResponse;
        var result = await _characteristic.WriteValueAsync(writer.DetachBuffer(), writeType);
        return result == GattCommunicationStatus.Success;
    }

    public ValueTask DisposeAsync()
    {
        _service.Dispose();
        _device.Dispose();
        return ValueTask.CompletedTask;
    }
}

internal static class Hex
{
    public static byte[] Decode(string value)
    {
        value = value.Replace("0x", "", StringComparison.OrdinalIgnoreCase)
            .Replace(" ", "")
            .Replace(",", "");
        if (value.Length == 0 || value.Length % 2 != 0) return Array.Empty<byte>();

        var bytes = new byte[value.Length / 2];
        for (var i = 0; i < bytes.Length; i++)
        {
            if (!byte.TryParse(value.Substring(i * 2, 2), NumberStyles.HexNumber, CultureInfo.InvariantCulture, out bytes[i]))
            {
                return Array.Empty<byte>();
            }
        }
        return bytes;
    }
}
