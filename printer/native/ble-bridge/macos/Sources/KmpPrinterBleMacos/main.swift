import CoreBluetooth
import Foundation

struct Options {
    let address: String
    let service: CBUUID
    let characteristic: CBUUID

    static func parse(_ args: [String]) -> Options? {
        if args.contains("--version") {
            print("kmp-printer-ble-macos 1.0.0")
            exit(0)
        }
        guard
            let address = value(after: "--connect", in: args),
            let service = value(after: "--service", in: args),
            let characteristic = value(after: "--characteristic", in: args)
        else {
            return nil
        }
        return Options(address: address, service: CBUUID(string: service), characteristic: CBUUID(string: characteristic))
    }

    private static func value(after key: String, in args: [String]) -> String? {
        guard let index = args.firstIndex(of: key), index + 1 < args.count else { return nil }
        return args[index + 1]
    }
}

final class BleBridge: NSObject, CBCentralManagerDelegate, CBPeripheralDelegate {
    private let options: Options
    private var central: CBCentralManager!
    private var peripheral: CBPeripheral?
    private var writable: CBCharacteristic?
    private var ready = false

    init(options: Options) {
        self.options = options
        super.init()
        central = CBCentralManager(delegate: self, queue: nil)
    }

    func run() {
        RunLoop.current.run(until: Date(timeIntervalSinceNow: 30))
        guard ready else {
            fputs("Unable to open BLE session.\n", stderr)
            exit(3)
        }

        print("READY")
        while let line = readLine() {
            let trimmed = line.trimmingCharacters(in: .whitespacesAndNewlines)
            if trimmed.uppercased() == "QUIT" { break }
            guard let data = Data(hex: trimmed), !data.isEmpty, let peripheral, let writable else {
                print("ERR invalid-state")
                continue
            }
            let writeType: CBCharacteristicWriteType = writable.properties.contains(.writeWithoutResponse) ? .withoutResponse : .withResponse
            peripheral.writeValue(data, for: writable, type: writeType)
            print("OK")
        }

        if let peripheral {
            central.cancelPeripheralConnection(peripheral)
        }
    }

    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        guard central.state == .poweredOn else { return }
        central.scanForPeripherals(withServices: [options.service], options: nil)
    }

    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        let identifier = peripheral.identifier.uuidString
        let name = peripheral.name ?? ""
        if identifier.caseInsensitiveCompare(options.address) == .orderedSame || name.caseInsensitiveCompare(options.address) == .orderedSame {
            self.peripheral = peripheral
            peripheral.delegate = self
            central.stopScan()
            central.connect(peripheral, options: nil)
        }
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        peripheral.discoverServices([options.service])
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let service = peripheral.services?.first(where: { $0.uuid == options.service }) else { return }
        peripheral.discoverCharacteristics([options.characteristic], for: service)
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        writable = service.characteristics?.first(where: { $0.uuid == options.characteristic })
        ready = writable != nil
        if ready {
            CFRunLoopStop(CFRunLoopGetCurrent())
        }
    }
}

extension Data {
    init?(hex: String) {
        let normalized = hex
            .replacingOccurrences(of: "0x", with: "")
            .replacingOccurrences(of: " ", with: "")
            .replacingOccurrences(of: ",", with: "")
        guard normalized.count % 2 == 0 else { return nil }
        var data = Data(capacity: normalized.count / 2)
        var index = normalized.startIndex
        while index < normalized.endIndex {
            let next = normalized.index(index, offsetBy: 2)
            guard let byte = UInt8(normalized[index..<next], radix: 16) else { return nil }
            data.append(byte)
            index = next
        }
        self = data
    }
}

guard let options = Options.parse(CommandLine.arguments) else {
    fputs("Usage: kmp-printer-ble-macos --connect <uuid-or-name> --service <uuid> --characteristic <uuid>\n", stderr)
    exit(2)
}

BleBridge(options: options).run()
