package searchengine.services.indexing;

public class AddressChanging {
    public static String getAddressWithoutW(String address) {
        StringBuilder builder = new StringBuilder();
        if (address.contains("www.")) {
            for (String part : address.split("www.")) builder.append(part.trim());
            return builder.toString();
        } else return address;
    }

    public static String getAddressForRepository(String address) {
        if (address.contains("//")) {
            String[] temp = address.split("//")[1].split("/");
            StringBuilder builder = new StringBuilder("/");
            for (int i = 1; i < temp.length; i++) {
                builder.append(temp[i].trim()).append("/");
            }
            return builder.toString();
        } else {
            return address;
        }
    }

    public static String getDomain(String siteUrl) {
        siteUrl = getAddressWithoutW(siteUrl);
        if (siteUrl.contains("//")) return siteUrl.split("//")[1];
        else return siteUrl;
    }
    public static String getNameDomain(String siteUrl) {
        //siteUrl = getDomain(siteUrl);
        return siteUrl.contains(".") ? getDomain(siteUrl).split("\\.")[0] : siteUrl;
    }
}
